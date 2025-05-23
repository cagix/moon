(ns cdq.application
  (:require [cdq.config :as config]
            [cdq.g :as g]
            [clojure.gdx.backends.lwjgl :as lwjgl])
  (:import (com.badlogic.gdx ApplicationAdapter)))

(def state (atom nil))

; We could _pass_ the whole application to -main / start!
; -> we don't need to know about 'cdq.g' / 'cdq.config' ....
; but not sure, we get an outdated context at stage !
; may need to share the atom itself with the stage ?
#_(defprotocol Application
  (on-create [_ libgdx-context])
  (on-dispose [_])
  (on-render [_])
  (on-resize [_ width height])
  (validate [_]))

(defn -main []
  (let [config (config/create "config.edn")]
    (lwjgl/application (:clojure.gdx.backends.lwjgl config)
                       (proxy [ApplicationAdapter] []
                         (create []
                           (reset! state ((requiring-resolve (:create config)) config))
                           (g/validate @state))

                         (dispose []
                           (g/validate @state)
                           ((requiring-resolve (:dispose config)) @state))

                         (render []
                           (g/validate @state)
                           (swap! state (requiring-resolve (:render config)))
                           (g/validate @state))

                         (resize [_width _height]
                           (g/validate @state)
                           (g/update-viewports! @state))))))
