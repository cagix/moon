(ns cdq.c
  (:require [cdq.application.create.record]
            [cdq.application.create.validation]
            [cdq.application.create.editor]
            [cdq.ui.editor.window]
            [cdq.application.create.handle-txs]
            [cdq.application.create.db]
            [cdq.application.create.vis-ui]
            [cdq.application.create.graphics]
            [cdq.application.create.stage]
            [cdq.application.create.input]
            [cdq.application.create.audio]
            [cdq.application.create.remove-files]
            [cdq.application.create.world]
            [cdq.application.create.reset-game-state]))

(defn create! [ctx]
  (-> ctx
      cdq.application.create.record/do!
      cdq.application.create.validation/do!
      cdq.application.create.editor/do!
      cdq.ui.editor.window/do!
      cdq.application.create.handle-txs/do!
      cdq.application.create.db/do!
      cdq.application.create.vis-ui/do!
      cdq.application.create.graphics/do!
      cdq.application.create.stage/do!
      cdq.application.create.input/do!
      cdq.application.create.audio/do!
      cdq.application.create.remove-files/do!
      cdq.application.create.world/do!
      cdq.application.create.reset-game-state/do!))
