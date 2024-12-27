(ns anvil.app
  (:require [cdq.context :as world]
            [clojure.gdx :as gdx :refer [clear-screen black]]
            [clojure.gdx.lwjgl :as lwjgl]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.context :as ctx]))

(def state (atom nil))

(def ^:private ^:dbg-flag pausing? true)

(defn -main []
  (let [{:keys [requires lwjgl3 lifecycle]} (-> "app.edn" io/resource slurp edn/read-string)]
    (run! require requires)
    (lwjgl/start lwjgl3
                 (reify lwjgl/Application
                   (create [_]
                     (reset! state (ctx/create-into (gdx/context)
                                                    [[:gdl.context/unit-scale 1]
                                                     [:gdl.context/assets "resources/"]
                                                     [:gdl.context/db (:db lifecycle)]
                                                     [:gdl.context/batch nil]
                                                     [:gdl.context/shape-drawer nil]
                                                     [:gdl.context/default-font (:default-font lifecycle)]
                                                     [:gdl.context/cursors (:cursors lifecycle)]
                                                     [:gdl.context/viewport (:viewport lifecycle)]
                                                     [:gdl.context/world-unit-scale (:tile-size lifecycle)]
                                                     [:gdl.context/world-viewport (:world-viewport lifecycle)]
                                                     [:gdl.context/tiled-map-renderer nil]
                                                     [:gdl.context/ui (:ui lifecycle)]
                                                     [:gdl.context/stage (fn [c] nil)]]))
                     (world/create @state (:world lifecycle)))

                   (dispose [_]
                     (ctx/cleanup @state))

                   (render [_]
                     (clear-screen black)
                     (let [context @state
                           stage (ctx/stage context)]
                       (world/render context)
                       (set! (.applicationState stage) context)
                       (.draw stage)
                       (.act stage))
                     (swap! state world/tick pausing?))

                   (resize [_ w h]
                     (ctx/resize @state w h))))))
