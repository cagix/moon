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

; for reset/change-lvl: (removed from first world/create)
; (world/dispose c) ; only for reset / change lvl

(defn- create-context [{:keys [gdl world]}]
  (let [context (ctx/create-into (gdx/context) gdl)]
    ; TODO how to pass world/widgets as configuration?
    ; just a list of 'components' /// ?
    ; :cdq.context/dev-menu
    ; etc. ?
    (world/create context world)))

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
