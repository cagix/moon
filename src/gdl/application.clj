(ns gdl.application)

(defprotocol Application
  (post-runnable! [_ f] "Posts a Runnable on the main loop thread."))
