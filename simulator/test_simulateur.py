import json
import re
import uuid
from datetime import datetime, timezone

import pytest

from simulateur import load_sensor_configs, telemetry_payload


DEVICE_ID_PATTERN = re.compile(r"^[a-z0-9-]{3,50}$")


def test_telemetry_payload_matches_contract():
    payload = telemetry_payload("sensor-room-01")

    assert payload["schemaVersion"] == 1
    assert uuid.UUID(payload["messageId"]).version == 4
    assert DEVICE_ID_PATTERN.fullmatch(payload["deviceId"])
    timestamp = datetime.fromisoformat(payload["timestamp"].replace("Z", "+00:00"))
    assert timestamp.tzinfo == timezone.utc
    assert payload["timestamp"].endswith("Z")

    measurements = {measurement["type"]: measurement for measurement in payload["measurements"]}
    assert set(measurements) == {"temperature", "humidity"}
    for measurement in measurements.values():
        assert set(measurement) == {"type", "value", "unit"}

    assert -40 <= measurements["temperature"]["value"] <= 85
    assert measurements["temperature"]["unit"] == "°C"
    assert 0 <= measurements["humidity"]["value"] <= 100
    assert measurements["humidity"]["unit"] == "%"


def test_telemetry_payload_can_generate_an_in_range_anomaly():
    payload = telemetry_payload("sensor-room-01", anomaly=True)
    temperature = next(item for item in payload["measurements"] if item["type"] == "temperature")

    assert temperature["value"] == 45.0
    assert -40 <= temperature["value"] <= 85


def test_load_sensor_configs_rejects_invalid_json():
    with pytest.raises(ValueError, match="SIMULATOR_SENSORS must be a JSON array"):
        load_sensor_configs("{")


def test_load_sensor_configs_rejects_duplicate_device_ids():
    sensors = [
        {"deviceId": "sensor-room-01", "intervalSec": 5, "password": "CHANGE_ME"},
        {"deviceId": "sensor-room-01", "intervalSec": 10, "password": "CHANGE_ME"},
    ]

    with pytest.raises(ValueError, match="Duplicate deviceId: sensor-room-01"):
        load_sensor_configs(json.dumps(sensors))


def test_load_sensor_configs_requires_a_password():
    with pytest.raises(ValueError, match="must have a non-empty password"):
        load_sensor_configs(
            json.dumps([{"deviceId": "sensor-room-01", "intervalSec": 5}])
        )


def test_load_sensor_configs_loads_credentials_without_changing_them():
    sensors = load_sensor_configs(
        json.dumps(
            [{"deviceId": "sensor-room-01", "intervalSec": 5, "password": "CHANGE_ME"}]
        )
    )

    assert len(sensors) == 1
    assert sensors[0].device_id == "sensor-room-01"
    assert sensors[0].interval_sec == 5
    assert sensors[0].password == "CHANGE_ME"
