(ns cdq.application.create
  (:require [cdq.application.create.record           :as create.record]
            [cdq.application.create.validation       :as create.validation]
            [cdq.application.create.editor           :as create.editor]
            [cdq.application.create.handle-txs       :as create.handle-txs]
            [cdq.application.create.db               :as create.db]
            [cdq.application.create.vis-ui           :as create.vis-ui]
            [cdq.application.create.graphics         :as create.graphics]
            [cdq.application.create.stage            :as create.stage]
            [cdq.application.create.input            :as create.input]
            [cdq.application.create.audio            :as create.audio]
            [cdq.application.create.remove-files     :as create.remove-files]
            [cdq.application.create.world            :as create.world]
            [cdq.application.create.reset-game-state :as create.reset-game-state]))

(defn do! [ctx]
  (-> ctx
      create.record/do!
      create.validation/do!
      create.editor/do!
      create.handle-txs/do!
      create.db/do!
      create.vis-ui/do!
      create.graphics/do!
      create.stage/do!
      create.input/do!
      create.audio/do!
      create.remove-files/do!
      create.world/do!
      create.reset-game-state/do!))
