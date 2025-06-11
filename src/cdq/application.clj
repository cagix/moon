(ns cdq.application
  (:require [cdq.malli :as m]
            [cdq.utils :as utils]
            clojure.edn
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
             [:ctx/gdl :some]
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

(def state (atom nil))

(defn create! [context create-fns]
  (let [ctx (reduce utils/render*
                    (merge (map->Context {})
                           context)
                    create-fns)]
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

(defn render! [render-fns]
  (swap! state (fn [ctx]
                 (m/validate-humanize schema ctx)
                 (let [ctx (reduce utils/render* ctx render-fns)]
                   (m/validate-humanize schema ctx)
                   ctx))))

(defn resize! [width height]
  (m/validate-humanize schema @state)
  (let [{:keys [ctx/graphics]} @state]
    (graphics/resize-viewports! graphics width height)))

(defn -main [config-path]
  (let [config (->> config-path
                    clojure.java.io/resource
                    slurp
                    clojure.edn/read-string
                    (clojure.walk/postwalk (fn [form]
                                             (if (symbol? form)
                                               (if (namespace form) ; var
                                                 (requiring-resolve form)
                                                 (do
                                                  (require form) ; namespace
                                                  form))
                                               form))))]
    (gdl.application.desktop/start!
     (assoc config :listener
            (reify gdl.application.desktop/Listener
              (create! [_ context]
                (create! context (:create-fns config)))
              (dispose! [_]
                (dispose!))
              (render! [_]
                (render! (:render-fns config)))
              (resize! [_ width height]
                (resize! width height)))))))
