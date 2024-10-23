(ns editor.widget
  (:require [editor.common :refer [widget-type]]))

(defmulti create widget-type)
(defmulti value  widget-type)
