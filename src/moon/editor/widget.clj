(ns ^:no-doc moon.editor.widget
  (:require [moon.editor.common :refer [widget-type]]))

(defmulti create widget-type)
(defmulti value  widget-type)
