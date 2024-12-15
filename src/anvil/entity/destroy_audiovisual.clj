(ns ^:no-doc anvil.entity.destroy-audiovisual
  (:require [anvil.component :as component]
            [anvil.world :as world]
            [gdl.db :as db]
            [gdl.utils :refer [defmethods]]))

(defmethods :entity/destroy-audiovisual
  (component/destroy [[_ audiovisuals-id] eid]
    (world/audiovisual (:position @eid)
                       (db/build audiovisuals-id))))
