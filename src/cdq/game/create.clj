(ns cdq.game.create
  (:require [cdq.game.create.tx-handler :as create-tx-handler]
            [cdq.game.create.db :as create-db]
            [cdq.game.create.graphics :as create-graphics]
            [cdq.game.create.ui :as create-ui]
            [cdq.game.create.input-processor :as create-input-processor]
            [cdq.game.create.audio :as create-audio]
            [cdq.game.create.world :as create-world]
            [qrecord.core :as q]))

(q/defrecord Context [])

(defn do! [gdx config]
  (-> {:ctx/gdx gdx}
      map->Context
      create-tx-handler/do!
      create-db/do!
      (create-graphics/do! (:graphics config))
      (create-ui/do! '[[cdq.ctx.create.ui.dev-menu/create cdq.game.create.world/do!]
                       [cdq.ctx.create.ui.action-bar/create]
                       [cdq.ctx.create.ui.hp-mana-bar/create]
                       [cdq.ctx.create.ui.windows/create [[cdq.ctx.create.ui.windows.entity-info/create]
                                                          [cdq.ctx.create.ui.windows.inventory/create]]]
                       [cdq.ctx.create.ui.player-state-draw/create]
                       [cdq.ctx.create.ui.message/create]])
      create-input-processor/do!
      (create-audio/do! (:audio config))
      (create-world/do! "world_fns/vampire.edn")))
