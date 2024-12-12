(ns gdl.assets-test
  (:require [gdl.app :as app]
            [gdl.app-test :refer [start-simple-app]]
            [gdl.assets :as assets]))

; why do I need to load all assets & do that searching thing?

(defn -main []
  (start-simple-app (reify app/Listener
                      (create [_]
                        (assets/setup)
                        (assets/play-sound "bfxr_caveenter"))
                      (dispose [_]
                        (assets/cleanup))
                      (render [_])
                      (resize [_ w h]))))
