(ns eu.stratuslab.cimi.views.status
  (:require [noir.core :refer [defpage pre-route]]
            [noir.response :as resp]
            [cemerick.friend :as friend]
            [net.cgrand.enlive-html :refer [deftemplate content]]
            [clojure.tools.logging :as log]))

(deftemplate status "eu/stratuslab/authn/vm_rest/views/status.html" []
  [:div#wrapper :p] (content "VM Service Status"))

(defpage "/" []
  (apply str (status)))
