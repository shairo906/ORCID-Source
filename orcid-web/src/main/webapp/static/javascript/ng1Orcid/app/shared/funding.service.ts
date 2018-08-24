import { Injectable } 
    from '@angular/core';

import { HttpClient, HttpClientModule, HttpHeaders } 
     from '@angular/common/http';

import { Observable, Subject } 
    from 'rxjs';


import { catchError, map, tap } 
    from 'rxjs/operators';

@Injectable()
export class FundingService {
    private headers: HttpHeaders;
    private notify = new Subject<any>();
    private fundingToEdit: any;
    
    public groups: any;
    public loading: any;

    notifyObservable$ = this.notify.asObservable();

    constructor( private http: HttpClient ){
        this.headers = new HttpHeaders(
            {
                'Access-Control-Allow-Origin':'*',
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': document.querySelector("meta[name='_csrf']").getAttribute("content")
            }
        );
        this.groups = null;
        this.fundingToEdit = {};
    }

    notifyOther(data: any): void {
        if (data) {
            this.notify.next(data);
        }
    }

    createNew(obj): any {
        var cloneF = JSON.parse(JSON.stringify(obj));
        cloneF.source = null;
        cloneF.putCode = null;
        for (var idx in cloneF.externalIdentifiers){
            cloneF.externalIdentifiers[idx].putCode = null;
        }
        return cloneF;
    }

    getDisambiguatedFunding( id ): Observable<any> {
        return this.http.get(
            getBaseUri() + '/fundings/disambiguated/id/' + id
        );
    }

    getEditable( putCode, groups ): any {
        // first check if they are the current source
        var funding = this.getFunding(putCode, groups);

        if (funding.source == orcidVar.orcidId){
            return funding;
        } else {
            var bestMatch = null;
            var group = this.getGroup(putCode, groups);
            for (var idx in group.activities) {
                if (group.activities[idx].source == orcidVar.orcidId) {
                    bestMatch = group.activities[idx];
                    break;
                }
            }
            if (bestMatch == null) {
                bestMatch = this.createNew(funding);
            }
            return bestMatch;
        }

    }

    getFunding(putCode?, groups?): any {
        if( putCode ){
            for (var idx in groups) {
                if (groups[idx].hasPut(putCode)){
                    return groups[idx].getByPut(putCode);
                }
            }
            return null;
            
        } else {
            this.getFundingEmpty();

        }
    }
    
    getFundingEmpty(): Observable<any> {
        return this.http.get(
            getBaseUri() + '/fundings/funding.json'
        )
    }

    getFundings(sort, sortAsc): Observable<any> {
        this.loading = true;
        return this.http.get(
            getBaseUri() + '/fundings/fundings.json?sort=' + sort + '&sortAsc=' + sortAsc
        )    
    }
    
    getFullFunding(putCode): Observable<any> {
        this.loading = true;
        return this.http.get(
            getBaseUri() + '/fundings/getFunding.json?fundingId=' + putCode
        )    
    }

    getFundingToEdit(): any {
        return this.fundingToEdit;
    }

    getGroup(putCode, groups): any {
        for (var idx in groups) {
            if (groups[idx].hasPut(putCode)){
                return groups[idx];
            }
        }
        return null;
    }

    getPublicFundings(sort, sortAsc): Observable<any> {
        this.loading = true;
        return this.http.get(
            getBaseUri() + '/' + orcidVar.orcidId + '/fundings.json?sort=' + sort + '&sortAsc=' + sortAsc
        )
    }

    updateToMaxDisplay(group, putCode): Observable<any> {
        return this.http.get(
            getBaseUri() + '/fundings/updateToMaxDisplay.json?putCode=' + putCode
        )
    }

    putFunding(obj) {
        let encoded_data = JSON.stringify(obj);
        return this.http.post( 
            getBaseUri() + '/fundings/funding.json',
            encoded_data,         
            { headers: this.headers }
        )
        .pipe(
            tap(
                (data) => {
                    //this.getData();
                    //groupedActivitiesUtil.rmByPut(funding.putCode.value, GroupedActivities.FUNDING,fundingSrvc.groups);                      
                }
            )
        )  
        ;
    }

    deleteFunding(obj): Observable<any> {
        return this.http.delete( 
            getBaseUri() + '/fundings/funding.json?id=' + encodeURIComponent(obj.putCode.value),           
            { headers: this.headers }
        )
    }

    serverValidate( obj, relativePath ): Observable<any> {
        let encoded_data = JSON.stringify(obj);
        return this.http.post( 
            getBaseUri() + '/' + relativePath, 
            encoded_data, 
            { headers: this.headers }
        );
    }

    setFundingToEdit(obj): void {
        this.fundingToEdit = obj;
    }

    updateProfileFunding(obj) {
        let encoded_data = JSON.stringify(obj);

        return this.http.post( 
            getBaseUri() + '/fundings/funding.json', 
            encoded_data,           
            { headers: this.headers }
        )
        .pipe(
            tap(
                (data) => {                    
                }
            )
        )  
        ;
    }

    updateVisibility(putCodes, priv): Observable<any> {
        let url = getBaseUri() + '/fundings/' + putCodes.splice(0,150).join() + '/visibility/'+priv;

        return this.http.get(
            url
        )
    }

}
