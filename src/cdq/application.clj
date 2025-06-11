(ns cdq.application
  (:require [cdq.malli :as m]
            [cdq.utils :as utils]
            clojure.edn
            gdl.application.lwjgl
            clojure.java.io
            clojure.walk
            [gdl.application.desktop]
            [gdl.graphics :as graphics]
            [gdl.utils.disposable :as disp]
            [qrecord.core :as q]))

(q/defrecord Context [ctx/config
                      ctx/db
                      ctx/graphics
                      ctx/stage])

(def ^:private schema
  (m/schema [:map {:closed true}
             [:ctx/config :some]
             [:ctx/input :some]
             [:ctx/db :some]
             [:ctx/audio :some]
             [:ctx/stage :some]
             [:ctx/graphics :some]
             [:ctx/world-event-handlers :some]
             [:ctx/elapsed-time number?]
             [:ctx/delta-time {:optional true} number?]
             [:ctx/max-delta number?]
             [:ctx/max-speed number?]
             [:ctx/minimum-size number?]
             [:ctx/paused? {:optional true} :boolean]
             [:ctx/tiled-map :some]
             [:ctx/grid :some]
             [:ctx/raycaster :some]
             [:ctx/content-grid :some]
             [:ctx/explored-tile-corners :some]
             [:ctx/id-counter :some]
             [:ctx/entity-ids :some]
             [:ctx/potential-field-cache :some]
             [:ctx/factions-iterations :some]
             [:ctx/z-orders :some]
             [:ctx/render-z-order :some]
             [:ctx/mouseover-eid {:optional true} :any]
             [:ctx/player-eid :some]
             [:ctx/active-entities {:optional true} :some]]))

(defn- create-config [config-path]
  (let [m (->> config-path
               clojure.java.io/resource
               slurp
               clojure.edn/read-string
               (clojure.walk/postwalk (fn [form]
                                        (if (symbol? form)
                                          (if (namespace form)
                                            (requiring-resolve form)
                                            (do
                                             (require form)
                                             form))
                                          form))))]
    #_(reify ILookup
      (valAt [_ k]
        (assert (contains? m k)
                (str "Config key not found: " k))
        (get m k)))
    m
    ))

(def state (atom nil))

(defn- create-listener [config]
  {:create! (fn []
              (let [context (gdl.application.desktop/create-context (:graphics config)
                                                                    (:user-interface config)
                                                                    (:audio config))
                    ctx (reduce utils/render*
                                (merge (map->Context {})
                                       (assoc context :ctx/config config))
                                (:create-fns config))]
                (m/validate-humanize schema ctx)
                (reset! state ctx)))

   :dispose! (fn []
               (let [{:keys [ctx/audio
                             ctx/graphics
                             ctx/tiled-map]} @state]
                 (disp/dispose! audio)
                 (disp/dispose! graphics)
                 (disp/dispose! tiled-map)
                 ; TODO vis-ui dispose
                 ; TODO what else disposable?
                 ; => :ctx/tiled-map definitely and also dispose when re-creting gamestate.
                 ))

   :render! (fn []
              (swap! state (fn [ctx]
                             (m/validate-humanize schema ctx)
                             (let [ctx (reduce utils/render*
                                               ctx
                                               (:render-fns config))]
                               (m/validate-humanize schema ctx)
                               ctx))))

   :resize! (fn [width height]
              (m/validate-humanize schema @state)
              (let [{:keys [ctx/graphics]} @state]
                (graphics/resize-viewports! graphics width height)))})

; this is much cleaner
; don't create abstractions for things which are only used 2x times 'somehow' repeated ...
; see at least a few times
; do not create unnecessary abstractions ...
(defn -main [config-path]
  (let [config (create-config config-path)]
    (gdl.application.lwjgl/start! (:lwjgl-config config)
                                  (create-listener config))))
