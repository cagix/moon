(ns cdq.application-state
  (:require [cdq.create.db]
            [cdq.create.gdx]
            [cdq.create.graphics]
            [cdq.create.stage]
            [cdq.create.ui-viewport]
            [gdl.application]
            [gdl.assets :as assets]
            [gdl.ui :as ui]))

; Maybe that's the trick
; => we access keys directly,
; but they are still protocols
; -> then graphics/etc. or 'level/world' should somehow come together?
; with 'cdq.g' we lose all dependencies ... everything is depenend on _one big thing_
; thats the issue
; there is not _one_big_thing_ ....

(defn create! [config]
  (ui/load! (:ui config))
  (-> (gdl.application/map->Context {})
      (assoc :ctx/config config)
      (cdq.create.graphics/add config)
      (cdq.create.gdx/add-gdx!)
      (cdq.create.ui-viewport/add config)
      (cdq.create.stage/add-stage!)
      (assoc :ctx/assets (assets/create (:assets config)))
      (cdq.create.db/add-db config)
      ((requiring-resolve 'cdq.game-state/create!))))
