import json
import subprocess
import platform
import os
import signal
from time import sleep


launch_json_path = ".vscode/launch.json"
frontend_path = "src/main/java/code/frontend"
application_path = "src/main/java/code/app.js"

processes = [None, None, None]




def launch_backend():
    with open(launch_json_path, "r") as launch_file:
        launch_configurations = json.load(launch_file)
    configuration = launch_configurations["configurations"][0]

    main_class = configuration.get("mainClass")
    arg_1, arg_2 = configuration.get("args", " ")
    args = [arg_1, arg_2]

    backend_command = ["mvn", "exec:java", f"-Dexec.mainClass={main_class}", f"-Dexec.args={args}"]
    processes[0] = subprocess.Popen(backend_command, preexec_fn=os.setsid)
    sleep(100/1000) # 100ms
    print("Backend running")



def launch_frontend():
    frontend_command = ["python", "-m", "http.server", "5500"]
    try: 
        processes[1] = subprocess.Popen(frontend_command, cwd=frontend_path, preexec_fn=os.setsid)
    except OSError: 
        print("Port already running")
    sleep(100/1000) # 100ms
    print("Frontend running")

    application_comand = ["npx", "electron", f"{application_path}"]
    processes[2] = subprocess.Popen(application_comand, preexec_fn=os.setsid)
    sleep(100/1000) # 100ms
    print("Application running")



def running():
    try:
        while True:
            for process in processes:
                if process and process.poll() is not None:
                    print("One process terminated. Cleaning up...")
                    kill_processes()
                    return
            sleep(1)

    except KeyboardInterrupt:
        print("Keyboard interupt noticed. Closing all processes...")
        kill_processes()


def kill_processes():
    for process in processes:
        if process and process.poll() is not None:
            try:
                os.killpg(os.getpgid(process.pid), signal.SIGTERM)
                print("Process shutdown")
            except ProcessLookupError:
                print("Process not found")
                continue
    print("Done.")



if __name__ == "__main__":
    launch_backend()
    launch_frontend()
    sleep(3)
    running()
