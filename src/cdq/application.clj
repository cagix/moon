(ns cdq.application
  (:require [cdq.db :as db]
            [cdq.g :as g]
            [cdq.malli :as m]
            [cdq.schemas :as schemas]
            [cdq.schemas-impl :as schemas-impl]
            [cdq.utils :as utils]
            [clojure.gdx.backends.lwjgl :as lwjgl]
            [gdl.application]
            [gdl.utils])
  (:import (com.badlogic.gdx ApplicationAdapter)))

(def ^:private ctx-schema
  (m/schema [:map {:closed true}
             [:assets :some]
             [:batch :some]
             [:unit-scale :some]
             [:world-unit-scale :some]
             [:shape-drawer-texture :some]
             [:shape-drawer :some]
             [:cursors :some]
             [:default-font :some]
             [:world-viewport :some]
             [:ui-viewport :some]
             [:tiled-map-renderer :some]
             [:stage :some]
             [:ctx/config :some]
             [:ctx/db :some]
             [:ctx/elapsed-time :some]
             [:ctx/delta-time {:optional true} number?]
             [:ctx/paused? {:optional true} :boolean]
             [:ctx/tiled-map :some]
             [:ctx/grid :some]
             [:ctx/raycaster :some]
             [:ctx/content-grid :some]
             [:ctx/explored-tile-corners :some]
             [:ctx/id-counter :some]
             [:ctx/entity-ids :some]
             [:ctx/potential-field-cache :some]
             [:ctx/mouseover-eid {:optional true} :any]
             [:ctx/player-eid :some]
             [:ctx/active-entities {:optional true} :some]]))

(extend-type gdl.application.Context
  g/Config
  (config [{:keys [ctx/config]} key]
    (get config key)))

(extend-type gdl.application.Context
  g/Database
  (get-raw [{:keys [ctx/db]} property-id]
    (db/get-raw db property-id))

  (build [{:keys [ctx/db] :as ctx} property-id]
    (db/build db property-id ctx))

  (build-all [{:keys [ctx/db] :as ctx} property-type]
    (db/build-all db property-type ctx))

  (property-types [{:keys [ctx/db]}]
    (schemas/property-types (:schemas db)))

  (schemas [{:keys [ctx/db]}]
    (:schemas db))

  (update-property! [ctx property]
    (update ctx :ctx/db db/update! property))

  (delete-property! [ctx property-id]
    (update ctx :ctx/db db/delete! property-id)))

(defn- create-config [path]
  (let [m (cdq.utils/io-slurp-edn path)]
    (reify clojure.lang.ILookup
      (valAt [_ k]
        (gdl.utils/safe-get m k)))))

(defn- create-schemas [path]
  (schemas-impl/create (utils/io-slurp-edn path)))

(def state (atom nil))

(defn reset-game-state! []
  (swap! state (requiring-resolve 'cdq.game-state/create!)))

(defn -main []
  (let [config (create-config "config.edn")
        create! (fn [config]
                  (-> (gdl.application/create-state! config)
                      (utils/safe-merge {:ctx/config config
                                         :ctx/db (db/create {:schemas (create-schemas (:schemas config))
                                                             :properties (:properties config)})})
                      ((requiring-resolve 'cdq.game-state/create!)))
                  #_(ctx-schema/validate @state))
        dispose! (requiring-resolve 'cdq.dispose/do!)
        render! (fn [ctx]
                  (m/validate-humanize ctx-schema ctx)
                  ((requiring-resolve 'cdq.render/do!) ctx))
        resize! (requiring-resolve 'cdq.resize/do!)]
    (run! require (:requires config))
    (lwjgl/application (:clojure.gdx.backends.lwjgl config)
                       (proxy [ApplicationAdapter] []
                         (create []
                           (reset! state (create! config)))

                         (dispose []
                           (dispose! @state))

                         (render []
                           (swap! state render!))

                         (resize [width height]
                           (resize! @state width height))))))
