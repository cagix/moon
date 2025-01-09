(ns cdq.app
  (:require [gdl.app :as app]
            [gdl.utils :as utils]
            [cdq.game :as game])
  (:gen-class))

; the 'no-private' rule

; namespaced keys for config keys too ?

(def state (atom nil))

(defn -main []
  (let [config (utils/read-edn-resource "cdq.app.edn")]
    (app/set-icon! (::icon config))
    ; TODO this to gdl.app & ns keys there
    (when (and #_(::glfw-async-on-mac-osx? config)
               app/mac-osx?)
      (app/set-glfw-to-async!))
    (app/start (::window config) ; this too just pass config like that again
               (reify app/Listener
                 (create [_ context]
                   (reset! state (game/create! (assoc context ::config config))))

                 (dispose [_]
                   (game/dispose! @state))

                 (render [_]
                   (swap! state game/render!))

                 (resize [_ width height]
                   (game/resize! @state width height))))))

(defn post-runnable [f]
  (app/post-runnable (:clojure.gdx/app @state)
                     #(f @state)))
