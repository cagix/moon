(comment

 ; project.clj
 ; [com.github.electronstudio/sdl2gdx "1.0.5"]

 (import 'com.badlogic.gdx.controllers.Controllers)
 (import 'uk.co.electronstudio.sdl2gdx.SDL2ControllerManager)
 (app/post-runnable
  (bind-root #'controllers (Controllers/getControllers))
  (println "Found controllers: " (count (seq controllers))))

 )
