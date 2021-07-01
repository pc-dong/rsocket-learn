import rsocketClient from './service/rsocket-client'
import React, {useState} from "react";


function Rsocket() {
    const [items, setItems] = useState(['']);
    const  loadItems = async () => {
        console.log('loadItems() called')
        const message = "Hello Rsocket!"
        const array: string[] = [];
        rsocketClient.splitString(message)
            .subscribe({
                onError: (error: Error) => console.error(error),
                onNext: (payload: any) => {
                    array.push(payload.data.toString());
                    setItems(array.concat());
                },
                onComplete: () => console.log("complete() called"),
                onSubscribe: (subscription: any) =>{
                    subscription.request(100)
                }
            });
    }

    return (
        <div className="Rsocket" >
            <button type="button" onClick={loadItems}>split [Hello Rsocket!]</button>
            {generateItems(items)}
        </div>)
}



function generateItems(items: string[]) {
   return  items.map((item, i) => {
        return (<li key={i}>{ item }</li>);
    });
}

export default Rsocket