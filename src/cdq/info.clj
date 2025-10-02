(ns cdq.info)

(defmulti info-text (fn [object world]
                      (cond (:item/slot object)
                            :info/item
                            ;(:skill/action-time object)
                            ;:info/skill
                            :else
                            :info/entity)))
