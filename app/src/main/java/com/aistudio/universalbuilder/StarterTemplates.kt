package com.aistudio.universalbuilder

object StarterTemplates {
    fun html(name:String):String {
        val body=when(name) {
            "Notes" -> """<textarea id="note" placeholder="Write your notes…"></textarea><p>Saved automatically on this device.</p><script>note.value=localStorage.getItem('note')||'';note.oninput=()=>localStorage.setItem('note',note.value);</script>"""
            "Photo gallery" -> """<input id="files" type="file" accept="image/*" multiple><div id="gallery"></div><script>let urls=[];files.onchange=()=>{urls.forEach(URL.revokeObjectURL);urls=[];gallery.replaceChildren();for(const f of files.files){const img=new Image();img.src=URL.createObjectURL(f);urls.push(img.src);img.style.width='46%';img.style.margin='2%';gallery.append(img);}}</script>"""
            "Audio player" -> """<input id="file" type="file" accept="audio/*"><audio id="player" controls style="width:100%"></audio><p>Select an audio file. This starter plays while the app is open.</p><script>let url;file.onchange=()=>{if(url)URL.revokeObjectURL(url);if(file.files[0]){url=URL.createObjectURL(file.files[0]);player.src=url;}}</script>"""
            else -> """<input id="title" placeholder="Product name"><input id="price" placeholder="Price" type="number"><button id="add">Add product</button><div id="items"></div><script>let products=JSON.parse(localStorage.getItem('products')||'[]');function render(){items.replaceChildren();products.forEach((p,i)=>{let row=document.createElement('p');row.textContent=p.name+' — '+p.price+' ';let del=document.createElement('button');del.textContent='Remove';del.onclick=()=>{products.splice(i,1);save()};row.append(del);items.append(row)})}function save(){localStorage.setItem('products',JSON.stringify(products));render()}add.onclick=()=>{if(title.value.trim()){products.push({name:title.value,price:price.value});save();title.value='';price.value=''}};render();</script>"""
        }
        return """<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"><title>$name</title><style>body{margin:0;padding:24px;background:#101012;color:#f5f5f5;font:16px system-ui}h1{font-size:32px;letter-spacing:-1px}input,textarea,button{box-sizing:border-box;padding:14px;border:1px solid #45454b;border-radius:12px;background:#222226;color:white;margin:8px 0}input,textarea{width:100%}textarea{height:55vh}button{background:white;color:black}p{line-height:1.6;color:#bbb}</style></head><body><h1>$name</h1>$body</body></html>"""
    }
}
