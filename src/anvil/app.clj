(ns anvil.app
  (:require [cdq.context :as world]
            [clojure.gdx :as gdx :refer [clear-screen black]]
            [clojure.gdx.lwjgl :as lwjgl]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.app :as app]
            [gdl.context :as ctx]))

(def ^:private ^:dbg-flag pausing? true)

(defn -main []
  (let [{:keys [requires lwjgl3 lifecycle]} (-> "app.edn" io/resource slurp edn/read-string)]
    (println "(run! require requires)")
    (time (run! require requires))
    (lwjgl/start lwjgl3
                 (reify lwjgl/Application
                   (create [_]
                     ; TODO pass vector because order is important
                     ; TODO delete 'Gdx'
                     ; TODO document 'c'
                     ; TODO make world without global state
                     ; TODO ui takes params?! - implicit ...
                     ; TODO context explorer in dev-menu ( & entity explorer )
                     ; ( & map tile explorer)
                     ; & dev-tools
                     ; & editor ?!
                     ; & map-editor ?!
                     ; its the editor itself - new game , etc .
                     ; rightclick edit sth /
                     ; pausing as part of state?
                     ; _defrecord namespaed keys!!! _
                     ; documented, arglist .. etc
                     ; only 'c' functions in gdl.context ...
                     ; schema not ...
                     (reset! app/state (ctx/create-into (gdx/context)
                                                        {:gdl.context/unit-scale 1
                                                         :gdl.context/assets "resources/"
                                                         :gdl.context/db (:db lifecycle)
                                                         :gdl.context/batch nil
                                                         ; TODO shape-drawer-texture separate
                                                         :gdl.context/shape-drawer nil
                                                         :gdl.context/default-font (:default-font lifecycle)
                                                         :gdl.context/cursors (:cursors lifecycle)
                                                         :gdl.context/viewport (:viewport lifecycle)
                                                         :gdl.context/tiled-map-renderer nil
                                                         :gdl.context/world-unit-scale (:tile-size lifecycle)
                                                         :gdl.context/world-viewport (:world-viewport lifecycle)
                                                         :gdl.context/ui (:ui lifecycle)
                                                         :gdl.context/stage (fn [c] nil)}))
                     (world/create @app/state (:world lifecycle)))

                   ; TODO restart won't work because create-into checks ccontains? 'safe-create-into?'

                   (dispose [_]
                     (ctx/cleanup @app/state))

                   (render [_]
                     (let [{:keys [gdl.context/stage] :as c} @app/state]
                       (clear-screen black)
                       (world/render c)
                       (.draw stage)
                       (.act stage))
                     (swap! app/state world/tick pausing?))

                   (resize [_ w h]
                     (ctx/resize @app/state w h))))))
