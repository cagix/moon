(ns gdl.fs)

(defprotocol FileSystem
  (internal [_ path]))
