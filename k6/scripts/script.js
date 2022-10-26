import http from 'k6/http';
import { check } from "k6";

export let options = {
    ext: {
        loadimpact: {
            projectID: 3606293,
            name: "streaming to cloud"
        }
    },
    duration: '1s',
    vus: 1,

    thresholds: {
        "http_req_duration": [`p(95) < ${__ENV.REQ_DURATION_THRESHOLDS}`],
    },
};

export default function () {
    let response = http.get("http://tiny-proxy-vm");
    let length = response.body.length
    check(length, { "body length ": (v) => v >= 0});
};
