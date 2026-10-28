import fs from 'node:fs';
const file=process.env.FIXTURES || 'load-test/fixtures.local.json';
const saved=JSON.parse(fs.readFileSync(file,'utf8'));
const base=(process.env.BASE_URL || saved.baseUrl).replace(/\/$/,'');
for(const user of [...(saved.loadUsers || []),...(saved.contenders || [])]) {
    if(!user.username) throw new Error('Fixture user is missing username; generate it with seed.mjs');
    const response=await fetch(base+'/api/v1/auth/login',{method:'POST',headers:{'Content-Type':'application/json'},
        body:JSON.stringify({username:user.username,password:process.env.LOAD_PASSWORD || 'load-test-123'})});
    if(response.status!==200) throw new Error('Cannot refresh fixture login');
    user.token=(await response.json()).data.accessToken;
}
saved.tokensRefreshedAt=new Date().toISOString();
fs.writeFileSync(file,JSON.stringify(saved,null,2)+'\n');
console.log('Refreshed tokens; dataset and account IDs unchanged.');
