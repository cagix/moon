(ns cdq.application-state
  (:require [cdq.create.assets]
            [cdq.create.config]
            [cdq.create.db]
            [gdl.application]
            [gdl.ui :as ui]))

(defn create! [config]
  (ui/load! (:ui config))
  (-> (gdl.application/create-state! config)
      (cdq.create.assets/add-assets config)
      (cdq.create.config/add-config config)
      (cdq.create.db/add-db config)
      ((requiring-resolve 'cdq.game-state/create!))))
