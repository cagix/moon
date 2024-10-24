(ns moon.item.modifiers
  (:require [moon.component :refer [defc] :as component]
            [moon.entity.modifiers :refer [mod-info-text]]))

(defc :item/modifiers
  {:schema [:s/components-ns :modifier]
   :let modifiers}
  (component/info [_]
    (when (seq modifiers)
      (mod-info-text modifiers))))
