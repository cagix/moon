(ns cdq.game.create
  (:require [cdq.game.create.record :as create-record]
            [cdq.game.create.get-gdx :as get-gdx]
            [cdq.game.create.tx-handler :as create-tx-handler]
            [cdq.game.create.db :as create-db]
            [cdq.game.create.graphics :as create-graphics]
            [cdq.game.create.ui :as create-ui]
            [cdq.game.create.input-processor :as create-input-processor]
            [cdq.game.create.audio :as create-audio]
            [cdq.game.create.world :as create-world]
            [clojure.config :as config]))

(defn do! []
  (let [config (config/edn-resource "config.edn")]
    (-> {}
        create-record/do!
        get-gdx/do!
        create-tx-handler/do!
        create-db/do!
        (create-graphics/do! (:graphics config))
        (create-ui/do! (:ui config))
        create-input-processor/do!
        (create-audio/do! (:audio config))
        (create-world/do! (:world config)))))
