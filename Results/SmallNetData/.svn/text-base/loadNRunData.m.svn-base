function[data1, data2,listing_dir,mutvals]=loadNRunData(runs)

maxtime=0;
%Get working directory
wd=cd;
listing = dir(wd)
size(listing)
columns_d=0;
%Find out maximum length and set endpoint
insert_point=1;
for counter=1:1:length(listing)
     if listing(counter).isdir
        if ~(strcmp(listing(counter).name, '.') || strcmp(listing(counter).name, '..') || strcmp(listing(counter).name, '.svn'))
            listing_dir(insert_point)=listing(counter);
            insert_point=insert_point+1;
        end
     end
end

data1=cell(size(listing_dir),runs);

for counter=1:1:length(listing_dir)
    dirname=strcat(strcat(wd,'\'),listing_dir(counter).name)
    cd(dirname);
    
    strorig=listing_dir(counter).name;
    pat = 'mutation';
    m = regexp(strorig, pat, 'split')
    mutvals(counter)=str2num(m{2});
    
    for runcounter=0:1:(runs-1)
        tempdata=load(strcat(strcat('log',num2str(runcounter)),'.log'));
        columns_d=size(tempdata,2);
        data1{counter,runcounter+1}=tempdata;
        tempmaxtime=max(tempdata(:,15));
        if tempmaxtime>maxtime
            maxtime=tempmaxtime;
        end
    end
end

cd(wd)

%Now let's set the data out in a form x(dir_it_came_from,exp,time,data)
data2=0;

number_type_runs=length(listing_dir);
number_runs_per_type=runs;
data_columns=columns_d;
time_points=0:1:maxtime;

data2=zeros(number_type_runs,number_runs_per_type,length(time_points),data_columns);
for list_counter=1:1:length(listing_dir)
    for run_counter=1:1:(runs)
        data2(list_counter,run_counter,:,1)=time_points;
        myvalues=data1{list_counter,run_counter};
        for time_counter=1:1:length(time_points)
            [r,c]=find(myvalues(:,15)<=time_points(time_counter));
            if(isempty(r))
                data2(list_counter,run_counter,time_counter,2:end)=zeros(1,14);
            else
                data2(list_counter,run_counter,time_counter,2:end)=myvalues(r(end,end),1:14);
            end
        end
        
    end
end
