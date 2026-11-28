from unittest.mock import Mock

import pytest

import simulateur


def test_parse_mqtt_tls_accepts_true_and_false():
    assert simulateur.parse_mqtt_tls("true") is True
    assert simulateur.parse_mqtt_tls("FALSE") is False


def test_parse_mqtt_tls_rejects_invalid_values():
    with pytest.raises(ValueError, match="MQTT_TLS must be true or false"):
        simulateur.parse_mqtt_tls("yes")


def test_create_mqtt_client_enables_tls_when_configured(monkeypatch):
    client = Mock()
    monkeypatch.setattr(simulateur.mqtt, "Client", Mock(return_value=client))
    sensor = simulateur.SensorConfig(
        "sensor-room-01", 5, "test-password")

    simulateur.create_mqtt_client(sensor, mqtt_tls=True)

    client.tls_set.assert_called_once_with()
    client.username_pw_set.assert_called_once_with(
        "sensor-room-01", "test-password")
