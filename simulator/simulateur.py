import json
import logging
import os
import random
import re
import signal
import threading
import time
import uuid
from dataclasses import dataclass
from datetime import datetime, timezone

import paho.mqtt.client as mqtt


DEVICE_ID_PATTERN = re.compile(r"^[a-z0-9-]{3,50}$")
DEFAULT_SENSORS = [
    {"deviceId": "sensor-room-01", "intervalSec": 5},
    {"deviceId": "sensor-room-02", "intervalSec": 10},
]
STOP_EVENT = threading.Event()


@dataclass(frozen=True)
class SensorConfig:
    device_id: str
    interval_sec: int


def utc_timestamp():
    return datetime.now(timezone.utc).isoformat(timespec="seconds").replace("+00:00", "Z")


def load_sensor_configs(raw_config):
    try:
        entries = json.loads(raw_config)
    except json.JSONDecodeError as error:
        raise ValueError("SIMULATOR_SENSORS must be a JSON array") from error

    if not isinstance(entries, list) or not entries:
        raise ValueError("SIMULATOR_SENSORS must contain at least one sensor")

    sensors = []
    seen_device_ids = set()
    for entry in entries:
        if not isinstance(entry, dict):
            raise ValueError("Each sensor must be a JSON object")

        device_id = entry.get("deviceId")
        interval_sec = entry.get("intervalSec")
        if not isinstance(device_id, str) or not DEVICE_ID_PATTERN.fullmatch(device_id):
            raise ValueError(f"Invalid deviceId: {device_id!r}")
        if device_id in seen_device_ids:
            raise ValueError(f"Duplicate deviceId: {device_id}")
        if isinstance(interval_sec, bool) or not isinstance(interval_sec, int) or not 1 <= interval_sec <= 3600:
            raise ValueError(f"intervalSec for {device_id} must be between 1 and 3600")

        seen_device_ids.add(device_id)
        sensors.append(SensorConfig(device_id, interval_sec))

    return sensors


def telemetry_payload(device_id):
    return {
        "schemaVersion": 1,
        "messageId": str(uuid.uuid4()),
        "deviceId": device_id,
        "timestamp": utc_timestamp(),
        "measurements": [
            {
                "type": "temperature",
                "value": round(random.uniform(18.0, 30.0), 1),
                "unit": "°C",
            },
            {
                "type": "humidity",
                "value": round(random.uniform(35.0, 65.0), 1),
                "unit": "%",
            },
        ],
    }


def status_payload(device_id, status, interval_sec=None, reason=None):
    payload = {
        "schemaVersion": 1,
        "deviceId": device_id,
        "status": status,
    }
    if status == "online":
        payload.update(
            {
                "timestamp": utc_timestamp(),
                "intervalSec": interval_sec,
                "enabled": True,
                "firmware": "sim-1.0.0",
            }
        )
    elif reason == "shutdown":
        payload["timestamp"] = utc_timestamp()
    if reason:
        payload["reason"] = reason
    return payload


def publish(client, topic, payload, retain=False):
    result = client.publish(
        topic,
        json.dumps(payload, separators=(",", ":")),
        qos=1,
        retain=retain,
    )
    if result.rc != mqtt.MQTT_ERR_SUCCESS:
        logging.warning("MQTT publish failed for %s (rc=%s)", topic, result.rc)
    return result


def run_sensor(sensor, broker_host, broker_port, heartbeat_interval_sec):
    topic_prefix = f"iot/devices/{sensor.device_id}"
    connected = threading.Event()
    next_heartbeat = 0.0
    client = mqtt.Client(
        callback_api_version=mqtt.CallbackAPIVersion.VERSION2,
        client_id=sensor.device_id,
        protocol=mqtt.MQTTv311,
    )
    client.will_set(
        f"{topic_prefix}/status",
        json.dumps(status_payload(sensor.device_id, "offline", reason="connection_lost")),
        qos=1,
        retain=True,
    )

    def on_connect(mqtt_client, _userdata, _flags, reason_code, _properties):
        nonlocal next_heartbeat
        if reason_code.is_failure:
            logging.warning("Connection rejected for %s: %s", sensor.device_id, reason_code)
            return
        connected.set()
        next_heartbeat = time.monotonic() + heartbeat_interval_sec
        publish(
            mqtt_client,
            f"{topic_prefix}/status",
            status_payload(sensor.device_id, "online", sensor.interval_sec),
            retain=True,
        )
        logging.info("%s connected; telemetry every %ss", sensor.device_id, sensor.interval_sec)

    def on_disconnect(_mqtt_client, _userdata, _disconnect_flags, _reason_code, _properties):
        connected.clear()

    client.on_connect = on_connect
    client.on_disconnect = on_disconnect

    while not STOP_EVENT.is_set():
        try:
            client.connect(broker_host, broker_port, keepalive=30)
            break
        except OSError as error:
            logging.warning("%s cannot reach MQTT broker: %s", sensor.device_id, error)
            STOP_EVENT.wait(5)

    if STOP_EVENT.is_set():
        return

    client.loop_start()
    next_telemetry = 0.0
    try:
        while not STOP_EVENT.is_set():
            if not connected.wait(timeout=0.5):
                continue

            now = time.monotonic()
            if now >= next_telemetry:
                publish(client, f"{topic_prefix}/telemetry", telemetry_payload(sensor.device_id))
                next_telemetry = now + sensor.interval_sec
            if now >= next_heartbeat:
                publish(
                    client,
                    f"{topic_prefix}/status",
                    status_payload(sensor.device_id, "online", sensor.interval_sec),
                    retain=True,
                )
                next_heartbeat = now + heartbeat_interval_sec

            wait_for = min(next_telemetry, next_heartbeat) - time.monotonic()
            STOP_EVENT.wait(max(0.0, wait_for))
    finally:
        if client.is_connected():
            result = publish(
                client,
                f"{topic_prefix}/status",
                status_payload(sensor.device_id, "offline", reason="shutdown"),
                retain=True,
            )
            if result.rc == mqtt.MQTT_ERR_SUCCESS:
                result.wait_for_publish(timeout=2)
            client.disconnect()
        client.loop_stop()


def main():
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
    raw_sensors = os.getenv("SIMULATOR_SENSORS") or json.dumps(DEFAULT_SENSORS)
    sensors = load_sensor_configs(raw_sensors)
    broker_host = os.getenv("MQTT_HOST", "localhost")
    broker_port = int(os.getenv("MQTT_PORT", "1883"))
    heartbeat_interval_sec = int(os.getenv("HEARTBEAT_INTERVAL_SEC", "30"))
    if not 1 <= heartbeat_interval_sec <= 3600:
        raise ValueError("HEARTBEAT_INTERVAL_SEC must be between 1 and 3600")

    def request_shutdown(_signal_number, _frame):
        STOP_EVENT.set()

    signal.signal(signal.SIGINT, request_shutdown)
    signal.signal(signal.SIGTERM, request_shutdown)

    threads = [
        threading.Thread(
            target=run_sensor,
            args=(sensor, broker_host, broker_port, heartbeat_interval_sec),
            name=sensor.device_id,
        )
        for sensor in sensors
    ]
    for thread in threads:
        thread.start()
    for thread in threads:
        thread.join()


if __name__ == "__main__":
    main()