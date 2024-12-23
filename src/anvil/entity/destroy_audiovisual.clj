(ns ^:no-doc anvil.entity.destroy-audiovisual
  (:require [anvil.component :as component]
            [gdl.context.db :as db]
            [anvil.world :as world]))

(defmethods :entity/destroy-audiovisual
  (component/destroy [[_ audiovisuals-id] eid]
    (world/audiovisual (:position @eid)
                       (db/build audiovisuals-id))))
