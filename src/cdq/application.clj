(ns cdq.application
  (:require [cdq.malli :as m]
            [cdq.utils :as utils]
            [gdl.graphics :as graphics]
            [gdl.utils.disposable :as disp]
            [qrecord.core :as q]))

(q/defrecord Context [ctx/config
                      ctx/db
                      ctx/graphics
                      ctx/input
                      ctx/stage])

(def ^:private schema
  (m/schema [:map {:closed true}
             [:ctx/config :some]
             [:ctx/db :some]
             [:ctx/audio :some]
             [:ctx/input :some]
             [:ctx/stage :some]
             [:ctx/graphics :some]
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
             [:ctx/active-entities {:optional true} :some]
             ]))

(def state (atom nil))

(defn create! []
  (let [config (utils/load-edn-config "config.edn")
        ctx (merge (map->Context {})
                   {:ctx/config config})
        ctx (reduce utils/render* ctx (:cdq.application/create-fns config))]
    (m/validate-humanize schema ctx)
    (reset! state ctx)))

(defn dispose! []
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

(defn render! []
  (swap! state (fn [ctx]
                 (m/validate-humanize schema ctx)
                 (let [ctx (reduce utils/render* ctx (:cdq.application/render-fns (:ctx/config ctx)))]
                   (m/validate-humanize schema ctx)
                   ctx))))

(defn resize! [width height]
  (m/validate-humanize schema @state)
  (let [{:keys [ctx/graphics]} @state]
    (graphics/resize-viewports! graphics width height)))
