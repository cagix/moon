(ns ^:no-doc moon.schema.number
  (:require [clojure.edn :as edn]
            [gdl.ui :as ui]
            [gdl.utils :refer [->edn-str]]
            [moon.schema :as schema])
  (:import (com.kotcrab.vis.ui.widget VisTextField)))

(defmethod schema/form :s/number  [_] number?)
(defmethod schema/form :s/nat-int [_] nat-int?)
(defmethod schema/form :s/int     [_] int?)
(defmethod schema/form :s/pos     [_] pos?)
(defmethod schema/form :s/pos-int [_] pos-int?)

(defmethod schema/widget :s/number [schema v]
  (ui/add-tooltip! (ui/text-field (->edn-str v) {})
                   (str schema)))

(defmethod schema/widget-value :s/number [_ widget]
  (edn/read-string (VisTextField/.getText widget)))
