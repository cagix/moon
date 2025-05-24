(ns cdq.application
  (:require [cdq.malli :as m]
            [cdq.utils]
            [clojure.gdx.backends.lwjgl :as lwjgl]
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

(defn- create-config [path]
  (let [m (cdq.utils/io-slurp-edn path)]
    (reify clojure.lang.ILookup
      (valAt [_ k]
        (gdl.utils/safe-get m k)))))

(def state (atom nil))

(defn reset-game-state! []
  (swap! state (requiring-resolve 'cdq.game-state/create!)))

(defn -main []
  (let [config (create-config "config.edn")
        create! (fn [config]
                  ((requiring-resolve 'cdq.application-state/create!) config)
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
