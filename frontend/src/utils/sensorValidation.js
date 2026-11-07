export function validateSensor(form, devices, editing = false) {
  const errors = {};
  if (!form.deviceId?.trim())
    errors.deviceId = "L’identifiant est obligatoire.";
  else if (!/^[a-z0-9-]{3,50}$/.test(form.deviceId))
    errors.deviceId = "3 à 50 caractères : minuscules, chiffres et tirets.";
  else if (
    !editing &&
    devices.some((device) => device.deviceId === form.deviceId)
  )
    errors.deviceId = "Cet identifiant est déjà utilisé.";
  if (!form.name?.trim()) errors.name = "Le nom est obligatoire.";
  if (
    !Number.isInteger(Number(form.intervalSec)) ||
    Number(form.intervalSec) < 1 ||
    Number(form.intervalSec) > 3600
  )
    errors.intervalSec = "Saisissez un entier entre 1 et 3600.";
  if (!form.measurementTypes?.length)
    errors.measurementTypes = "Sélectionnez au moins un type de mesure.";
  return errors;
}
