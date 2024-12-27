(ns anvil.app
  (:require [cdq.context :as world]
            [clojure.gdx :as gdx :refer [clear-screen black]]
            [clojure.gdx.lwjgl :as lwjgl]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.context :as ctx]))

(def state (atom nil))

(def ^:private ^:dbg-flag pausing? true)

; TODO
; * move state out of world/create
; * dev-menu restart world
; * change world

;; * create initial world context
;; * reset to initial-world context
;; * change world (but keep player&inventoryui,etc....)
;; * first remove all other state / vars like player message ....

(defn- create-context [config]
  (let [context (ctx/create-into (gdx/context)
                                 [[:gdl.context/unit-scale 1]
                                  [:gdl.context/batch]
                                  [:gdl.context/assets "resources/"]
                                  [:gdl.context/db (:db config)]
                                  [:gdl.context/shape-drawer]
                                  [:gdl.context/default-font (:default-font config)]
                                  [:gdl.context/cursors (:cursors config)]
                                  [:gdl.context/viewport (:viewport config)]
                                  [:gdl.context/world-unit-scale (:tile-size config)]
                                  [:gdl.context/world-viewport (:world-viewport config)]
                                  [:gdl.context/tiled-map-renderer]
                                  [:gdl.context/ui (:ui config)]
                                  [:gdl.context/stage (fn [c] nil)]])]
    (world/create context (:world config))))

(defn -main []
  (let [{:keys [requires lwjgl3 context]} (-> "app.edn" io/resource slurp edn/read-string)]
    (run! require requires)
    (lwjgl/start lwjgl3
                 (reify lwjgl/Application
                   (create [_]
                     (reset! state (create-context context)))

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
