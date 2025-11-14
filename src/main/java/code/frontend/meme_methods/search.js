
let sortingType = "id";

const sortLabels = {
    id: "Oldest",
    reverse_id: "Newest",
    title: "Alphabetical",
    reverse_title: "Alphabetical (inverse)",
    likes: "Most Liked",
    reverse_likes: "Least Liked"
};


async function searchMemes() {
    try {

        // Get variables
        let query = document.getElementById("query").value;

        // Create content header
        let content = {
            method: "GET",
        };
        
        document.getElementById("result").textContent = "Finding...";

        // Send the GET request
        const response = await fetch(
            "/api/meme/search?search_query=" + query + "&sorting_type=" + sortingType, 
            content
        );

        // Display memes
        let memes = await response.json();
        for (let meme of memes) {
            display_meme(meme);
        }

        document.getElementById("result").textContent = "Success";
        console.log("Success");

    } catch (error) {
        document.getElementById("result").textContent = "Something went wrong";
        console.error(error);
    }
}


function display_meme(memeJson) {
    let meme = document.createElement("img");

    meme.src = memeJson.path;
    meme.width = memeJson.width;
    meme.height = memeJson.height;
    meme.alt = memeJson.title;

    document.body.appendChild(meme);
}


function setSort(type) {
    sortingType = type;

    const label = sortLabels[type] || "Newest";
    document.getElementById("sortingType").textContent = label;
}