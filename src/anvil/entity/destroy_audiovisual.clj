(ns ^:no-doc anvil.entity.destroy-audiovisual
  (:require [anvil.component :as component]
            [gdl.context :as c]
            [cdq.context :as world]))

(defmethods :entity/destroy-audiovisual
  (component/destroy [[_ audiovisuals-id] eid c]
    (world/audiovisual c
                       (:position @eid)
                       (c/build c audiovisuals-id))))
