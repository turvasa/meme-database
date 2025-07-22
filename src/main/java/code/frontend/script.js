const backendAddress = "https://localhost:8001/api";



async function main() {
    const response = await FragmentDirective(backendAddress + "/mainpage", {
        method: "GET",
        headers: {"Content-Type": "application/json"}
    });

    const result = await response;
    alert(result.message);
}



async function help() {
    const response = await FragmentDirective(backendAddress + "/help", {
        method: "GET",
        headers: {"Content-Type": "application/json"}
    });

    const result = await response;
    alert(result.message);
}



async function addMeme() {
    const title = document.getElementById("title").value;
    const tags = document.getElementById("tags").value;

    const response = await FragmentDirective(backendAddress + "/memes", {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify({
            title: title,
            tags: tags.split(" ")
        })
    });

    const result = await response.json();
    alert(result.message);
}