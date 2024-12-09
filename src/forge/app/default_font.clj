(ns forge.app.default-font
  (:require [anvil.disposable :as disposable]
            [anvil.graphics :as g]
            [clojure.utils :refer [bind-root]]))

(defn create [[_ font]]
  (bind-root g/default-font (g/truetype-font font)))

(defn dispose [_]
  (disposable/dispose g/default-font))
