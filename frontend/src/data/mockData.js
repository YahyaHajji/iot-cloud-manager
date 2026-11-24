const catalogue = {
  temperature: { unit: '°C', min: -40, max: 85 }, humidity: { unit: '%', min: 0, max: 100 },
  pressure: { unit: 'hPa', min: 300, max: 1100 }, co2: { unit: 'ppm', min: 0, max: 10000 },
  water_volume: { unit: 'L', min: 0 }, presence: { unit: 'bool', min: 0, max: 1 },
}
export const measurementTypes = Object.keys(catalogue)
export const measurementCatalog = catalogue
const seed = [
  ['sensor-room-01','Salle serveur','Bâtiment A',['temperature','humidity'],true,5],
  ['sensor-room-02','Salle de réunion','Bâtiment A',['temperature','co2','presence'],true,10],
  ['sensor-room-03','Bureau direction','Bâtiment A',['temperature','humidity'],true,15],
  ['sensor-room-04','Accueil','Bâtiment A',['temperature','presence'],true,5],
  ['sensor-room-05','Salle électrique','Bâtiment A',['temperature','humidity'],false,30],
  ['sensor-room-06','Open space','Bâtiment B',['temperature','humidity','co2'],true,10],
  ['sensor-room-07','Atelier','Bâtiment B',['temperature','humidity'],true,5],
  ['sensor-room-08','Stockage','Bâtiment B',['temperature','humidity'],false,60],
  ['sensor-room-09','Couloir nord','Bâtiment B',['temperature','presence'],true,15],
  ['sensor-room-10','Couloir sud','Bâtiment B',['temperature','presence'],true,15],
  ['sensor-greenhouse-01','Serre principale','Extérieur',['temperature','humidity'],true,5],
  ['sensor-greenhouse-02','Serre semis','Extérieur',['temperature','humidity'],true,5],
  ['sensor-greenhouse-03','Serre tropicale','Extérieur',['temperature','humidity','co2'],true,10],
  ['sensor-greenhouse-04','Réserve végétale','Extérieur',['temperature','humidity'],false,30],
  ['water-meter-01','Compteur principal','Sous-sol',['water_volume'],true,60],
  ['water-meter-02','Compteur annexe','Sous-sol',['water_volume'],true,60],
  ['sensor-pressure-01','Station météo','Toit',['pressure','temperature'],true,30],
  ['sensor-pressure-02','Local technique','Sous-sol',['pressure','temperature'],false,30],
  ['sensor-co2-01','Laboratoire','Bâtiment C',['co2','temperature'],true,10],
  ['sensor-co2-02','Salle de classe','Bâtiment C',['co2','temperature'],true,10],
  ['sensor-presence-01','Entrée principale','Bâtiment A',['presence'],true,5],
  ['sensor-presence-02','Parking','Extérieur',['presence'],false,15],
  ['sensor-room-11','Cuisine','Bâtiment C',['temperature','humidity'],true,10],
  ['sensor-room-12','Archives','Bâtiment C',['temperature','humidity'],true,30],
]
export const mockDevices = seed.map(([deviceId,name,location,types,enabled,intervalSec],i)=>({
  deviceId,name,location,description:`Capteur ${name.toLowerCase()}`,status:i%6===4||i%7===6?'offline':'online',enabled,intervalSec,firmware:'sim-1.0.0',
  lastSeenAt:new Date(Date.now()-((i%8)*4+1)*60000).toISOString(),measurementTypes:types,
}))
const uuid=()=>crypto.randomUUID()
export const mockMeasurements=mockDevices.flatMap((d,i)=>Array.from({length:8},(_,j)=>{
  const timestamp=new Date(Date.now()-(7-j)*3600000-i*13000).toISOString()
  return {schemaVersion:1,messageId:uuid(),deviceId:d.deviceId,timestamp,measurements:d.measurementTypes.map(type=>{
    const ranges={temperature:[19,34],humidity:[35,72],pressure:[990,1035],co2:[420,1250],water_volume:[15,140],presence:[0,1]}
    const [min,max]=ranges[type];let value=+(min+((i*17+j*13)%100)/100*(max-min)).toFixed(type==='water_volume'?1:1)
    if(type==='temperature'&&i===0&&j===6)value=31
    return {type,value,unit:catalogue[type].unit}
  })}
}))
mockDevices.forEach(device => {
  const latest = [...mockMeasurements].filter(row => row.deviceId === device.deviceId).sort((a,b) => b.timestamp.localeCompare(a.timestamp))[0]
  device.lastMeasurements = latest?.measurements.map(measurement => ({ ...measurement, timestamp: latest.timestamp })) ?? []
})
export const mockAlerts=[
 {id:1,deviceId:'sensor-room-01',severity:'warning',title:'Température élevée',detail:'31 °C · Seuil 27 °C',time:'Il y a 12 min',state:'active'},
 {id:2,deviceId:'sensor-co2-02',severity:'danger',title:'CO₂ élevé',detail:'1 250 ppm · Seuil 1 000 ppm',time:'Il y a 28 min',state:'active'},
 {id:3,deviceId:'sensor-room-05',severity:'info',title:'Capteur hors ligne',detail:'Aucune communication récente',time:'Il y a 1 h',state:'active'},
 {id:4,deviceId:'sensor-greenhouse-02',severity:'warning',title:'Humidité basse',detail:'36 % · Seuil 40 %',time:'Il y a 2 h',state:'resolved'},
]
