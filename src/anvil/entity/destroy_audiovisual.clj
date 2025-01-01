(ns ^:no-doc anvil.entity.destroy-audiovisual
  (:require [gdl.context :as c]
            [cdq.context :as world]
            [clojure.component :as component :refer [defcomponent]]))

(defcomponent :entity/destroy-audiovisual
  (component/destroy [[_ audiovisuals-id] eid c]
    (world/audiovisual c
                       (:position @eid)
                       (c/build c audiovisuals-id))))
