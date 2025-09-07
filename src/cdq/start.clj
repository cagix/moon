(ns cdq.start
  (:require [cdq.application :as application]
            [cdq.ctx :as ctx]
            [cdq.malli :as m]
            [clojure.edn :as edn]
            [clojure.gdx.backends.lwjgl :as lwjgl]
            [clojure.gdx.scenes.scene2d :as scene2d]
            [clojure.gdx.utils.shared-library-loader :as shared-library-loader]
            [clojure.java.io :as io]
            [qrecord.core :as q])
  (:gen-class))

(defmacro def-record-and-schema [record-sym & ks]
  `(do
    (q/defrecord ~record-sym
      ~(mapv (comp symbol first) ks))
    (def schema
      [:map {:closed true} ~@ks])))

(def-record-and-schema Context
  [:ctx/active-entities :some]
  [:ctx/audio :some]
  [:ctx/batch :some]
  [:ctx/config :some]
  [:ctx/content-grid :some]
  [:ctx/cursors :some]
  [:ctx/db :some]
  [:ctx/default-font :some]
  [:ctx/delta-time :some]
  [:ctx/draw-fns :some]
  [:ctx/draw-on-world-viewport :some]
  [:ctx/elapsed-time :some]
  [:ctx/entity-ids :some]
  [:ctx/explored-tile-corners :some]
  [:ctx/factions-iterations :some]
  [:ctx/graphics :some]
  [:ctx/grid :some]
  [:ctx/id-counter :some]
  [:ctx/info :some]
  [:ctx/input :some]
  [:ctx/max-delta :some]
  [:ctx/max-speed :some]
  [:ctx/minimum-size :some]
  [:ctx/mouseover-eid :any]
  [:ctx/paused? :any]
  [:ctx/player-eid :some]
  [:ctx/potential-field-cache :some]
  [:ctx/raycaster :some]
  [:ctx/render-layers :some]
  [:ctx/render-z-order :some]
  [:ctx/schema :some]
  [:ctx/shape-drawer :some]
  [:ctx/shape-drawer-texture :some]
  [:ctx/stage :some]
  [:ctx/textures :some]
  [:ctx/tiled-map :some]
  [:ctx/tiled-map-renderer :some]
  [:ctx/ui-actors :some]
  [:ctx/ui-viewport :some]
  [:ctx/unit-scale :some]
  [:ctx/world-unit-scale :some]
  [:ctx/world-viewport :some]
  [:ctx/z-orders :some]
  )

(extend-type Context
  clojure.gdx.scenes.scene2d/Context
  (handle-draws! [ctx draws]
    (ctx/handle-draws! ctx draws)))

(defn- execute! [[f params]]
  ((requiring-resolve f) params))

(defn- create-listener
  [{:keys [create-pipeline
           dispose-fn
           render-pipeline
           resize-fn]}]
  (let [create! (fn []
                  (reduce (fn [ctx f]
                            (let [result (if (vector? f)
                                           (let [[f params] f]
                                             ((requiring-resolve f) ctx params))
                                           ((requiring-resolve f) ctx))]
                              (if (nil? result)
                                ctx
                                result)))
                          (map->Context {:schema (m/schema schema)})
                          create-pipeline))
        dispose! (fn [ctx]
                   (requiring-resolve dispose-fn) ctx)
        render! (fn [ctx]
                  (reduce (fn [ctx f]
                            (if-let [new-ctx ((requiring-resolve f) ctx)]
                              new-ctx
                              ctx))
                          ctx
                          render-pipeline))
        resize! (fn [ctx width height]
                  ((requiring-resolve resize-fn) ctx width height))]
    {:create! (fn []
                (reset! application/state (create!)))
     :dispose! (fn []
                 (dispose! @application/state))
     :render! (fn []
                (swap! application/state render!))
     :resize! (fn [width height]
                (resize! @application/state width height))
     :pause! (fn [])
     :resume! (fn [])}))

(defn -main []
  (let [{:keys [operating-sytem->executables
                listener
                config]} (-> "cdq.start.edn" io/resource slurp edn/read-string)]
    (->> (shared-library-loader/operating-system)
         operating-sytem->executables
         (run! execute!))
    (lwjgl/start-application! (create-listener listener)
                              config)))
