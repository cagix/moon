(ns gdl.graphics.animation-test
  (:require ;[gdl.app-test :refer [start-simple-app]]
            [gdl.context.assets :as assets]
            [gdl.graphics :as g]))

; how do I just draw a texture/sprite first?!
; and why does it need a 'world-viewport' ?
; => assets get a texture
; => sprite needs a texture they don't know about each other

(def graphics
  {:viewport {:width 800
              :height 600}
   :world-viewport {:tile-size 48
                    :width 600
                    :height 600}})

#_(defn -main []
    (start-simple-app (reify app/Listener
                        (create [_]
                          (assets/setup "resources/")
                          (g/setup graphics)
                          (graphics/setup-shape-drawer))
                        (dispose [_]
                          (assets/cleanup)
                          (g/cleanup)
                          (graphics/dispose-shape-drawer))
                        (render [_]
                          (g/clear)
                          (g/draw-with g/viewport
                                       1
                                       (fn []
                                         (g/filled-rectangle 200 200 100 100 g/white))))
                        (resize [_ w h]
                          (g/resize w h)))))

; for many apps it is duplicated effeort
; always setup/cleanup/resize ....
; can make a protocol ... applicationlistener.....
; => also graphics too big
