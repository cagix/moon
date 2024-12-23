(ns gdl.assets-test
  (:require ;[gdl.app-test :refer [start-simple-app]]
            [gdl.context :as ctx]))

; why do I need to load all assets & do that searching thing?

#_(defn -main []
  (start-simple-app (reify app/Listener
                      (create [_]
                        (ctx/assets-setup)
                        (ctx/play-sound "bfxr_caveenter"))
                      (dispose [_]
                        (ctx/assets-cleanup))
                      (render [_])
                      (resize [_ w h]))))
