
async function addMeme() {
    try {
        document.getElementById("buttonText").textContent = "   Adding..."

        // Get the variables
        let title = document.getElementById("title").value.trim();
        let tags = document.getElementById("tags").value.split(" ");
        let image = document.getElementById("memeFile").files[0];

        // Check variables validity
        let regex = new RefExp("[\w-]", "gi");
        checkTitleValidity(title, regex);
        checkTagsValidity(tags, regex);
        checkFileValidity(file);


        // Convert variables to JSON
        let memeJson = JSON.stringify({
            title: title,
            tags: getTagsArray(tags)
        })

        // Convert meme file to FormData
        let data = new FormData();
        data.append("meme", memeJson)
        data.append("image", image);

        // Create the content header
        const content = {
            method: "POST",
            credentials: "include",
            body: data
        };

        // Send the POST request
        const response = await fetch("/api/memes", content);

        /// Get results
        const result = await response.json();
        document.getElementById("buttonText").textContent = "Meme added succesfully.";
        alert(result.message);
    } 
    
    catch (error) {
        if (error.includes("Invalid title")) {
            document.getElementById("titleException").textContent = error;
        }

        else if (error.includes("Invalid tag")) {
            document.getElementById("tagsException").textContent = error;
        }

        else if (error.includes("Invalid file")) {
            document.getElementById("fileException").textContent = error;
        }

        else document.getElementById("buttonText").textContent = "Unable to add the meme. Please try again.";

        console.error(error);
    }
}



function getTagsArray(tags) {
    let index = 1;
    tagsArray = [];

    for (let tag of tags) {
        tagsArray.push({
            title: tag, 
            count: "0"
        });
    }

    return tagsArray;
}



function checkTitleValidity(title, regex) {

    // Check length
    if (title.length > 50) {
        throw "Invalid title: length can't exceed 30 chars.";
    }

    // Match for regex
    if (!title.match(regex)) {
        throw "Invalid title: can only contain alphabets, numbers, '_' and '-'."
    }

    // Check and relpace white space
    if (title.includes(" ")) {
        document.getElementById("titleException").textContent = "All white space in the title are replaced by '-' automaticly"
        title = title.replace(" ", "_")
    }

    // Reset exception message
    document.getElementById("titleException").textContent = "";
}


function checkTagsValidity(tags, regex) {

    // Iterate all tags
    for (let tag of tags) {

        // Check length
        if (tag.length > 20) {
            throw "Invalid tag: length can't exceed 20 chars.";
        }

        // Match for regex
        if (!tag.match(regex)) {
            throw "Invalid tag: can only contain alphabets, numbers, '_' and '-'."
        }
    }

    // Reset exception message
    document.getElementById("tagException").textContent = "";
}



function checkFileValidity(file) {
    let allowedExtensions = "(\.jpg|\.jpeg|\.png|\.gif)$/i";

    // Check file validity
    if (!allowedExtensions.exec(file.value)) {
        throw "Invalid file: must be .jpg, .jpeg, .png or .gif"
    }
}
