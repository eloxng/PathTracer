# Path Tracer
A path tracer that calculates the shortest distance to your selected destination from your current location with the RuneLite API.

**Successful generated paths**
![path1success](https://github.com/user-attachments/assets/594004d3-18a1-4429-8d20-e7bd85b7fc57)
![path2success](https://github.com/user-attachments/assets/711c73cd-3117-47c0-a322-68dee3f906d7)
![path3success](https://github.com/user-attachments/assets/5aba6cf1-c3ab-4529-a58c-514ecc8df417)

**Existing Bugs**
- When entering a new scene, the path generated in the previous scene carries over and is rendered again in the new scene
- _Crashes_ occur when...
  - The destination is selected behind a closed, walkable door
    - When door is open, path generates like it should
  - The path should generate a U-shaped curve (i.e. walking around a building)
 
**Work in Progress**
- Selecting paths through the worldmap
- Fixing crashes
