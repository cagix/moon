(ns anvil.app
  (:require [cdq.context :as world]  ; TODO only cdq.context :as ctx !
            ; and this one uses gdl.context and gdx
            [clojure.gdx :refer [clear-screen black]]
            [clojure.gdx.lwjgl :as lwjgl]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.context :as ctx]))

(def state (atom nil))

(def ^:private ^:dbg-flag pausing? true)

; TODO
; * dev-menu restart world
; * change world

; Operations:
; * create initial world context
; * reset to initial-world context
; * change world (but keep player&inventoryui,etc....)

; * remove all other global state / vars like player message

; for reset/change-lvl: (removed from first world/create)
; (world/dispose c) ; only for reset / change lvl

(defn -main []
  (let [{:keys [requires lwjgl3 context]} (-> "app.edn" io/resource slurp edn/read-string)]
    (run! require requires)
    (lwjgl/start lwjgl3
                 (reify lwjgl/Application
                   (create [_]
                     (reset! state (world/create context)))

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
