(ns gdl.assets-test
  (:require ;[gdl.app-test :refer [start-simple-app]]
            [gdl.context :as ctx]
            [gdl.context.assets :as assets]))

; why do I need to load all assets & do that searching thing?

#_(defn -main []
  (start-simple-app (reify app/Listener
                      (create [_]
                        (assets/setup "resources/")
                        (ctx/play-sound (ctx/get-ctx) "bfxr_caveenter"))
                      (dispose [_]
                        (assets/cleanup))
                      (render [_])
                      (resize [_ w h]))))
