//src/App.tsx
import  { useEffect, useState } from 'react';
import client from './api/client';

export default function App() {

    // Declaramos elestado 'status' y su funcion para actualizarlo, inicializandolo con null
    const [status, setStatus ] = useState<string | null>(null);
    // Hook useEffect que se ejecuta al montar componente
    useEffect(() => {
        // Realizamos un Get a cliente axios para obtener el estado del backend
        client.get('/api/health')
        // Si tenemos respuesta actualizamos el esto
            .then(response => { setStatus(response.data.status);})
            // si ocurre un error, actualizamos el estado a 'Error'
            .catch(error => {setStatus('Error');});
    }, []);

    // Renderiza el componente: titul y el estado recibido del backend
    return (
        <div>
            <h1>BoardWar</h1>
            <pre>{status}</pre>
        </div>
    )
}
