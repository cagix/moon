(ns cdq.resizable)

(defprotocol Resizable
  (resize! [_ width height]))

